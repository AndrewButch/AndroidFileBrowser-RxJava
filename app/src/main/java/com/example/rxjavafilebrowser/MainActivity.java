package com.example.rxjavafilebrowser;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.jakewharton.rxbinding4.view.RxView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import kotlin.Unit;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    FileListAdapter adapter;
    private CompositeDisposable viewSubscriptions;
    private FIleBrowserViewModel viewModel;

    @Override
    protected void onResume() {
        super.onResume();
        viewSubscriptions.add(
                viewModel.getCurrentDirFiles()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::updateList));
    }

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
    protected void onPause() {
        super.onPause();
        viewSubscriptions.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.unsubscribe();
    }


    private void init() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup views
        final ListView listView = findViewById(R.id.list_view);
        adapter = new FileListAdapter(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listView.setAdapter(adapter);
        final Button rootBtn = findViewById(R.id.root_btn);
        final Button backBtn = findViewById(R.id.back_btn);

        viewSubscriptions = new CompositeDisposable();

        // Emits file with selected dir path
        Observable<File> listViewObservable =
                Observable.create(emitter ->
                        listView.setOnItemClickListener((parent, view, position, id) -> {
                            final File file = (File) view.getTag();
                            Log.d(TAG, "Selected dir: " + file);
                            if (file.isDirectory()) {
                                emitter.onNext(file);
                            }
                        }));

        // Emits file with root path
        Observable<Unit> rootButtonObservable =
                RxView.clicks(rootBtn);

        // Emits file with parent path
        Observable<Unit> backButtonObservable =
                RxView.clicks(backBtn);

        FileBrowserModel model = new FileBrowserModel(
                this::createFilesObservable,
                Environment.getExternalStorageDirectory().getPath(),
                getPreferences(MODE_PRIVATE));

        viewModel = new FIleBrowserViewModel(
                listViewObservable,
                rootButtonObservable,
                backButtonObservable,
                model);
        viewModel.subscribe();

    }

    private void updateList(List<File> files) {
        adapter.clear();
        adapter.addAll(files);
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

}