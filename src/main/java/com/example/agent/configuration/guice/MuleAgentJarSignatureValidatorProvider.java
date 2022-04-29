package com.example.agent.configuration.guice;

import com.google.inject.Binder;
import com.mulesoft.agent.configuration.guice.BaseModuleProvider;
import com.mulesoft.agent.services.ApplicationValidator;
import com.example.agent.MuleAgentJarSignatureValidator;

public class MuleAgentJarSignatureValidatorProvider extends BaseModuleProvider {
    @Override
    protected void configureModule(Binder binder) {
        bindNamedSingleton(binder, ApplicationValidator.class, MuleAgentJarSignatureValidator.class);
    }
}