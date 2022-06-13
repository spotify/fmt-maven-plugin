String buildLog = new File("${basedir}/build.log").getText("UTF-8")
// Would want to assert that it's 'ERROR....Non complying', but could not get the regex to work.
assert buildLog.contains("Non complying file")
