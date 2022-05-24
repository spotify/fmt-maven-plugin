String buildLog = new File("${basedir}/build.log").getText("UTF-8")
assert !buildLog.contains("non-complying files")