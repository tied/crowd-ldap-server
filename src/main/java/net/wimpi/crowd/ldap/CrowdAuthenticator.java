package net.wimpi.crowd.ldap;

import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.service.client.CrowdClient;
import java.nio.charset.StandardCharsets;
import javax.naming.AuthenticationException;
import net.wimpi.crowd.ldap.util.Utils;
import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.authn.AbstractAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CrowdAuthenticator
        extends AbstractAuthenticator {

    private final Logger logger = LoggerFactory.getLogger(CrowdAuthenticator.class);

    private final CrowdClient crowdClient;
    private final SchemaManager schemaManager;
    private final Dn crowdDn;
    private final Dn usersDn;

    public CrowdAuthenticator(CrowdClient crowdClient, SchemaManager schemaManager)
            throws LdapInvalidDnException {

        super(AuthenticationLevel.SIMPLE);
        this.crowdClient = crowdClient;
        this.schemaManager = schemaManager;

        this.crowdDn = new Dn(schemaManager, Utils.CROWD_DN);
        this.usersDn = new Dn(schemaManager, Utils.CROWD_USERS_DN);
    }

    public LdapPrincipal authenticate(BindOperationContext context)
            throws Exception {

        if ((context.getDn().getParent().equals(usersDn) || context.getDn().getParent().equals(crowdDn)) && (
                context.getDn().getRdn().getType().equals(SchemaConstants.UID_AT) ||
                        context.getDn().getRdn().getType().equals(SchemaConstants.UID_AT_OID) ||
                        context.getDn().getRdn().getType().equals(SchemaConstants.CN_AT) ||
                        context.getDn().getRdn().getType().equals(SchemaConstants.CN_AT_OID) ||
                        context.getDn().getRdn().getType().equals(SchemaConstants.COMMON_NAME_AT))) {

            String userId = context.getDn().getRdn().getNormValue();
            String password = new String(context.getCredentials(), StandardCharsets.UTF_8);

            try {

                User user = crowdClient.authenticateUser(userId, password);

                logger.debug("The user {} has been successfully authenticated.", user);
                return new LdapPrincipal(schemaManager, context.getDn(), AuthenticationLevel.SIMPLE);

            } catch (Exception e) {

                logger.debug("Authentication could not be performed.", e);
                throw e;
            }

        } else {

            AuthenticationException error =
                    new AuthenticationException("Cannot handle unexpected DN : " + context.getDn());

            logger.debug("Authentication could not be performed with an incorrect DN.", error);
            throw error;
        }
    }
}
