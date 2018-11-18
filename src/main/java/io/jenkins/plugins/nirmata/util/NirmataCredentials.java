
package io.jenkins.plugins.nirmata.util;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;

import java.util.*;

import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.model.Item;
import hudson.security.ACL;

public class NirmataCredentials {

    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(NirmataCredentials.class);
    private Item _project;

    @SuppressWarnings("unused")
    private NirmataCredentials() {
    }

    public NirmataCredentials(Item project) {
        _project = project;
    }

    public List<StringCredentials> getCredentials() {
        return lookupCredentials(
            StringCredentials.class, _project,
            ACL.SYSTEM, Collections.<DomainRequirement> emptyList());
    }

    public Optional<StringCredentials> getCredential(String credentialId) {
        List<StringCredentials> idCredentials = getCredentials();
        CredentialsMatcher matcher = CredentialsMatchers.withId(credentialId);
        return Optional.ofNullable(CredentialsMatchers.firstOrNull(idCredentials, matcher));
    }
}
