package com.example.rxjavafilebrowser;

import java.io.File;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import kotlin.Unit;

public class FIleBrowserViewModel {
    private final CompositeDisposable subscriptions = new CompositeDisposable();

    private Observable<File> listItemObservable;
    private Observable<Unit> rootButtonObservable;
    private Observable<Unit> backButtonObservable;
    private FileBrowserModel model;

    private BehaviorSubject<List<File>> currentDirFiles;
    private Observable<File> selectedDir;
    private File systemRoot;

    public FIleBrowserViewModel(
            Observable<File> listItemObservable,
            Observable<Unit> rootButtonObservable,
            Observable<Unit> backButtonObservable,
            FileBrowserModel model) {
        this.listItemObservable = listItemObservable;
        this.rootButtonObservable = rootButtonObservable;
        this.backButtonObservable = backButtonObservable;
        this.model = model;
    }

    public void subscribe() {
        currentDirFiles = BehaviorSubject.create();

        selectedDir = model.getSelectedDir();

        final Observable<File> rootObservable =
                rootButtonObservable
                        .map(event -> model.getDefaultPath());

        final Observable<File> backObservable = backButtonObservable
                .withLatestFrom(selectedDir, (unit, file) -> file.getParentFile());


        subscriptions.add(
                Observable
                        .merge(
                                listItemObservable,
                                rootObservable,
                                backObservable)
                        .subscribe(model::putSelectedFile)
        );

        subscriptions.add(
                model.getFilesList()
                        .subscribe(currentDirFiles::onNext)
        );
    }

    public void unsubscribe() {
        subscriptions.clear();
    }

    public Observable<List<File>> getCurrentDirFiles() {
        return currentDirFiles.hide();
    }
}
