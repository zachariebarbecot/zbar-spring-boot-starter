assert new File(basedir, 'target/surefire-reports/TEST-fr.zbar.it.SampleTest.xml').exists()
assert new File(basedir, 'target/failsafe-reports/TEST-fr.zbar.it.SampleIT.xml').exists()
assert new File(basedir, 'target/site/jacoco').listFiles().any { it.name == 'index.html' }
assert !(new File(basedir, 'build.log').text =~ /jacoco-maven-plugin:[^:]+:check/)
