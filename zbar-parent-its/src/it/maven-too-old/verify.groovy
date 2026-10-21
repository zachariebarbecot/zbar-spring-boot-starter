def log = new File(basedir, 'build.log').text
assert log.contains('RequireMavenVersion failed')
assert log.contains('[999,)')
