package editor

import (
	"fmt"
	"os"
	"os/exec"
	"runtime"
	"strings"
)

func Edit(pattern string, content []byte) ([]byte, string, error) {
	file, err := os.CreateTemp("", pattern)
	if err != nil {
		return nil, "", err
	}
	path := file.Name()
	if _, err := file.Write(content); err != nil {
		file.Close()
		return nil, path, err
	}
	if err := file.Close(); err != nil {
		return nil, path, err
	}

	command := strings.Fields(editorCommand())
	editor := exec.Command(command[0], append(command[1:], path)...)
	editor.Stdin = os.Stdin
	editor.Stdout = os.Stdout
	editor.Stderr = os.Stderr
	if err := editor.Run(); err != nil {
		return nil, path, fmt.Errorf("run editor %s: %w", command[0], err)
	}

	edited, err := os.ReadFile(path)
	return edited, path, err
}

func editorCommand() string {
	for _, name := range []string{"QUEUE_EDITOR", "VISUAL", "EDITOR"} {
		if value := strings.TrimSpace(os.Getenv(name)); value != "" {
			return value
		}
	}
	if runtime.GOOS == "windows" {
		return "notepad"
	}
	return "vi"
}
