package editor

import (
	"bytes"
	"fmt"
	"os"
	"os/exec"
	"strings"
)

type Save func(content []byte) error

const errorPrefix = "# error: "

func Edit(title, pattern string, content []byte, save Save) (bool, error) {
	command := strings.Fields(os.Getenv("QUEUE_EDITOR"))
	if len(command) == 0 {
		return editBuiltIn(title, content, save)
	}
	return editExternal(command, pattern, content, save)
}

func editExternal(command []string, pattern string, content []byte, save Save) (bool, error) {
	file, err := os.CreateTemp("", pattern)
	if err != nil {
		return false, err
	}
	path := file.Name()
	if err := file.Close(); err != nil {
		return false, err
	}

	previous := content
	shown := content
	for {
		if err := os.WriteFile(path, shown, 0o600); err != nil {
			return false, err
		}

		editor := exec.Command(command[0], append(command[1:], path)...)
		editor.Stdin = os.Stdin
		editor.Stdout = os.Stdout
		editor.Stderr = os.Stderr
		if err := editor.Run(); err != nil {
			return false, fmt.Errorf("run editor %s: %w\nyour changes are saved in %s", command[0], err, path)
		}

		edited, err := os.ReadFile(path)
		if err != nil {
			return false, err
		}
		edited = withoutError(edited)
		if bytes.Equal(edited, content) {
			os.Remove(path)
			return false, nil
		}
		if bytes.Equal(edited, previous) && !bytes.Equal(shown, previous) {
			return false, fmt.Errorf("nothing changed after the error, your changes are saved in %s", path)
		}

		err = save(edited)
		if err == nil {
			os.Remove(path)
			return true, nil
		}
		previous = edited
		shown = append(errorComment(err), edited...)
	}
}

func errorComment(err error) []byte {
	var b strings.Builder
	for _, line := range strings.Split(err.Error(), "\n") {
		b.WriteString(errorPrefix + line + "\n")
	}
	b.WriteString(errorPrefix + "fix it and save again, or save without changes to cancel\n")
	return []byte(b.String())
}

func withoutError(content []byte) []byte {
	for bytes.HasPrefix(content, []byte(errorPrefix)) {
		end := bytes.IndexByte(content, '\n')
		if end < 0 {
			return nil
		}
		content = content[end+1:]
	}
	return content
}
