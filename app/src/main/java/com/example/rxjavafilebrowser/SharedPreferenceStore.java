package com.example.rxjavafilebrowser;

import android.content.SharedPreferences;

import androidx.arch.core.util.Function;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class SharedPreferenceStore<T> {
    private final BehaviorSubject<T> subject;

    public SharedPreferenceStore(
            SharedPreferences preferences,
            String key,
            String defaultValue,
            Function<T, String> serialize,
            Function<String, T> deserialize) {

        T initialValue = deserialize.apply(preferences.getString(key, defaultValue));

        subject = BehaviorSubject.createDefault(initialValue);
        subject
                .subscribe(T ->
                        preferences
                                .edit()
                                .putString(key, serialize.apply(T))
                                .commit());
    }

    public void put(T value) {
        subject.onNext(value);
    }

    public Observable<T> getStream() {
        return subject.hide();
    }
}
