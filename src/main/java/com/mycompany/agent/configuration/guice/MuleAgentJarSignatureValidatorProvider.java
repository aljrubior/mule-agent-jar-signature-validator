package com.mycompany.agent.configuration.guice;

import com.google.inject.Binder;
import com.mulesoft.agent.configuration.guice.BaseModuleProvider;
import com.mulesoft.agent.services.ArtifactValidator;
import com.mycompany.agent.MuleAgentJarSignatureValidator;

public class MuleAgentJarSignatureValidatorProvider extends BaseModuleProvider {
    @Override
    protected void configureModule(Binder binder) {
        bindNamedSingleton(binder, ArtifactValidator.class, MuleAgentJarSignatureValidator.class);
    }
}