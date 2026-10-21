def reports = new File(basedir, 'target/surefire-reports')
assert new File(reports, 'TEST-fr.zbar.it.SampleTest.xml').exists()
assert !new File(reports, 'TEST-fr.zbar.it.SampleIT.xml').exists()
assert !new File(basedir, 'target/failsafe-reports').exists()
