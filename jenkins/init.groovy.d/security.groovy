import jenkins.model.Jenkins

// Set up security
def jenkins = Jenkins.getInstance()
jenkins.setSecurityRealm(hudson.security.SecurityRealm.fromHudson('none'))
jenkins.save()
println "Security configured"
