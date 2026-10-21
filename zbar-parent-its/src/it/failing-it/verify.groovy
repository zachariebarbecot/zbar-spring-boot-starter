def log = new File(basedir, 'build.log').text
assert log.contains('maven-failsafe-plugin')
assert log.contains('fr.zbar.it.SampleIT')
assert log.contains('Tests run: 1, Failures: 1')
assert new File(basedir, 'target/surefire-reports/TEST-fr.zbar.it.SampleTest.xml').exists()
