package com.iwobanas.screenrecorder;

import android.content.Context;

import com.google.analytics.tracking.android.StandardExceptionParser;

import java.util.Collection;

public class AnalyticsExceptionParser extends StandardExceptionParser {
    public AnalyticsExceptionParser(Context context,
                                    Collection<String> additionalPackages) {
        super(context, additionalPackages);
    }

    @Override
    public String getDescription(String threadName, Throwable t) {
        return getDescription(getCause(t), getBestStackTraceElement(getCause(t)), threadName);
    }

    protected String getDescription(Throwable cause, StackTraceElement element, String threadName) {
        StringBuilder descriptionBuilder = new StringBuilder();
        descriptionBuilder.append(cause.getClass().getSimpleName());
        if (element != null) {
            descriptionBuilder.append(String.format(" (@%s:%s:%s)", element.getClassName(), element.getMethodName(), element.getLineNumber()));
        }

        if (threadName != null) {
            descriptionBuilder.append(String.format(" {%s}", threadName.replaceAll("[0-9]*", "XX")));
        }

        return descriptionBuilder.toString();
    }

}