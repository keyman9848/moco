package com.github.dreamhead.moco.recorder;

import com.github.dreamhead.moco.HttpRequest;
import com.github.dreamhead.moco.MocoConfig;
import com.github.dreamhead.moco.Request;
import com.github.dreamhead.moco.ResponseHandler;
import com.github.dreamhead.moco.handler.AbstractResponseHandler;
import com.github.dreamhead.moco.internal.SessionContext;
import com.google.common.collect.ImmutableList;

import static com.github.dreamhead.moco.MocoRecorders.group;

public class DynamicReplayHandler extends AbstractResponseHandler {
    private RecorderRegistry registry;
    private RecorderIdentifier identifier;
    private RecorderModifier modifier;

    public DynamicReplayHandler(final RecorderConfigurations configurations) {
        this.registry = configurations.getRecorderRegistry();
        this.identifier = configurations.getIdentifier();
        this.modifier = configurations.getModifier();
    }

    private HttpRequest getRequiredRecordedRequest(final HttpRequest request) {
        HttpRequest recordedRequest = getRecordedRequest(request);
        if (recordedRequest == null) {
            throw new IllegalArgumentException("No recorded request for [" + identifier + "]");
        }
        return recordedRequest;
    }

    protected final HttpRequest getRecordedRequest(final HttpRequest request) {
        String name = this.identifier.getIdentifier(request);
        RequestRecorder recorder = registry.recorderOf(name);
        return recorder.getRequest();
    }

    @Override
    public void writeToResponse(final SessionContext context) {
        Request request = context.getRequest();
        HttpRequest recordedRequest = getRequiredRecordedRequest((HttpRequest) request);
        SessionContext newContext = new SessionContext(recordedRequest, context.getResponse());
        modifier.writeToResponse(newContext);
    }

    protected ResponseHandler doApply(final MocoConfig config) {
        RecorderModifier applied = this.modifier.apply(config);

        if (applied != this.modifier) {
            RecorderConfigurations configurations = RecorderConfigurations.create(ImmutableList.<RecorderConfig>of(
                    group(this.registry.getGroup()),
                    this.identifier,
                    modifier
            ));

            return new DynamicReplayHandler(configurations);
        }

        return this;
    }
}