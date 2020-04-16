package com.example.testing;

@SuppressWarnings("unused")
class UserConstructorClass {
    private final double data;

    public UserConstructorClass(
        @JavaParameters(
            @JavaParameter(@JavaTag("Huge Number"))
        ) double data) {
        this.data = data;
    }

    double getData() {
        return data;
    }
}
