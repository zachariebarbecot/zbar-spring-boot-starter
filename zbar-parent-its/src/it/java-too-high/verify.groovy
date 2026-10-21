def log = new File(basedir, 'build.log').text
assert log.contains('RequireJavaVersion failed')
assert log.contains('[999,)')
