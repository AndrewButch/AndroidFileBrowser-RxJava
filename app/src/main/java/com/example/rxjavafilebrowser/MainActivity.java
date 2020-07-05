package com.example.rxjavafilebrowser;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.jakewharton.rxbinding4.view.RxView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    FileListAdapter adapter;
    private CompositeDisposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        } else {
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.clear();
    }

    private Observable<List<File>> createFilesObservable(final File file) {
        return Observable.create(emitter -> {
            try {
                List<File> fileList = getFiles(file);
                emitter.onNext(fileList);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    private List<File> getFiles(final File f) {
        List<File> fileList = new ArrayList<>();
        File[] files = f.listFiles();

        if (files != null) {
            for (File file : files) {
                if (!file.isHidden() && file.canRead()) {
                    fileList.add(file);
                }
            }
        }

        return fileList;
    }

    private void init() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ListView listView = findViewById(R.id.list_view);
        adapter = new FileListAdapter(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listView.setAdapter(adapter);

        final File root = new File(Environment.getExternalStorageDirectory().getPath());

        Button rootBtn = findViewById(R.id.root_btn);
        Button backBtn = findViewById(R.id.back_btn);

        disposable = new CompositeDisposable();

        BehaviorSubject<File> selectedDir = BehaviorSubject.createDefault(root);
        disposable.add(selectedDir
                .switchMap(
                        file -> createFilesObservable(file)
                                .subscribeOn(Schedulers.io())
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateList));

        Observable<File> listViewObservable =
                Observable.create(emitter ->
                        listView.setOnItemClickListener((parent, view, position, id) -> {
                            final File file = (File) view.getTag();
                            Log.d(TAG, "Selected dir: " + file);
                            if (file.isDirectory()) {
                                emitter.onNext(file);
                            }
                        }));


        Observable<File> rootButtonObservable =
                RxView.clicks(rootBtn)
                        .map(event -> root);

        Observable<File> backButtonObservable =
                RxView.clicks(backBtn)
                        .map(event -> selectedDir.getValue().getParentFile());

        Observable<File> changeDirObservable =
                Observable.merge(
                        listViewObservable,
                        rootButtonObservable,
                        backButtonObservable);
        disposable.add(changeDirObservable.subscribe(selectedDir::onNext));

    }

    private void updateList(List<File> files) {
        adapter.clear();
        adapter.addAll(files);
    }

}