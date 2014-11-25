package com.iwobanas.screenrecorder;

import android.content.Context;
import android.media.MediaCodec;

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
        if (cause instanceof IllegalStateException || cause instanceof MediaCodec.CodecException) {
            descriptionBuilder.append(": ");
            descriptionBuilder.append(cause.getMessage());
        }
        if (element != null) {
            if (cause instanceof IllegalStateException)
            descriptionBuilder.append(String.format(" at %s.%s(SourceFile:%s)", element.getClassName(), element.getMethodName(), element.getLineNumber()));
        }

        if (threadName != null) {
            descriptionBuilder.append(String.format(" {%s}", threadName.replaceAll("\\d+", "XX")));
        }

        return descriptionBuilder.toString();
    }

}