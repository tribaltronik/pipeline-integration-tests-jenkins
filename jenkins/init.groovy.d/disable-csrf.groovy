import jenkins.model.Jenkins

// Disable CSRF
def jenkins = Jenkins.getInstance()
jenkins.setCrumbIssuer(null)
jenkins.save()
println "CSRF disabled"
