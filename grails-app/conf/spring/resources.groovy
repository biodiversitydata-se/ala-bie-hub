import org.pac4j.core.config.Config

// Place your Spring DSL code here
beans = {
    // SBDI: Application won't start without this. It is related to security.oidc.enabled
    // being set to false since we don't use it.
    myConfig(Config)
}
