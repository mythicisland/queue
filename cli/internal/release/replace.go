package release

import (
	"errors"
	"fmt"
	"io/fs"
	"os"
	"path/filepath"
)

// Replace moves the running binary aside first, because Windows can't overwrite a running exe but can rename it.
func Replace(binary []byte) (string, error) {
	path, err := os.Executable()
	if err != nil {
		return "", err
	}
	path, err = filepath.EvalSymlinks(path)
	if err != nil {
		return "", err
	}

	old := path + ".old"
	next := path + ".new"
	os.Remove(old)

	if err := os.WriteFile(next, binary, 0o755); err != nil {
		return "", permission(err, path)
	}
	if err := os.Rename(path, old); err != nil {
		os.Remove(next)
		return "", permission(err, path)
	}
	if err := os.Rename(next, path); err != nil {
		os.Rename(old, path)
		os.Remove(next)
		return "", permission(err, path)
	}
	os.Remove(old)
	return path, nil
}

func permission(err error, path string) error {
	if errors.Is(err, fs.ErrPermission) {
		return fmt.Errorf("no permission to replace %s, run the upgrade with sudo", path)
	}
	return err
}
