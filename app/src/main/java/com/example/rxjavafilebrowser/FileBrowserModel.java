package com.example.rxjavafilebrowser;

import android.content.SharedPreferences;

import java.io.File;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FileBrowserModel {
    private final String PREF_CURRENT_DIR_KEY = "CURRENT_DIR";

    private Observable<List<File>> filesListObservable;
    private SharedPreferenceStore<File> store;
    private String defaultPath;

    public FileBrowserModel(
            Function<File, Observable<List<File>>> getFiles,
            String defaultPath,
            SharedPreferences preferences) {

        this.defaultPath = defaultPath;

        store = new SharedPreferenceStore<>(
                preferences,
                PREF_CURRENT_DIR_KEY,
                defaultPath,
                File::getAbsolutePath,
                File::new);


        filesListObservable = store
                .getStream()
                .switchMap(file -> getFiles.apply(file))
                .subscribeOn(Schedulers.io());

    }

    public Observable<File> getSelectedDir() {
        return store.getStream();
    }

    public Observable<List<File>> getFilesList() {
        return filesListObservable;
    }

    public void putSelectedFile(File file) {
        store.put(file);
    }

    public File getDefaultPath() {
        return new File(defaultPath);
    }
}
