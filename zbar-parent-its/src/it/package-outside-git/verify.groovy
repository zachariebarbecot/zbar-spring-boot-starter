for (def dir = basedir; dir != null; dir = dir.parentFile) {
  assert !new File(dir, '.git').exists() : "project is inside a git repository: ${dir}"
}
assert new File(basedir, 'target/package-outside-git-1.0.jar').exists()
