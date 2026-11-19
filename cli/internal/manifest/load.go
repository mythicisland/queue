package manifest

import (
	"fmt"
	"io/fs"
	"os"
	"path/filepath"
	"regexp"
	"strings"

	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
)

var documentSeparator = regexp.MustCompile(`(?m)^---\s*$`)

func Load(path string) ([]*queuev1.QueueType, error) {
	files, err := files(path)
	if err != nil {
		return nil, err
	}

	var queueTypes []*queuev1.QueueType
	origins := map[string]string{}
	for _, file := range files {
		data, err := Read(file)
		if err != nil {
			return nil, err
		}

		for _, document := range documentSeparator.Split(string(data), -1) {
			if strings.TrimSpace(document) == "" {
				continue
			}
			queueType, err := Decode([]byte(document))
			if err != nil {
				return nil, fmt.Errorf("%s: %w", file, err)
			}

			name := queueType.GetName()
			if name == "" {
				return nil, fmt.Errorf("%s: a queue type has no name", file)
			}
			if origin, exists := origins[name]; exists {
				return nil, fmt.Errorf("%s: queue type %s is already defined in %s", file, name, origin)
			}
			origins[name] = file
			queueTypes = append(queueTypes, queueType)
		}
	}
	return queueTypes, nil
}

func files(path string) ([]string, error) {
	if path == "-" {
		return []string{path}, nil
	}

	info, err := os.Stat(path)
	if err != nil {
		return nil, err
	}
	if !info.IsDir() {
		return []string{path}, nil
	}

	var files []string
	err = filepath.WalkDir(path, func(file string, entry fs.DirEntry, err error) error {
		if err != nil {
			return err
		}
		switch strings.ToLower(filepath.Ext(file)) {
		case ".yaml", ".yml", ".json":
			if !entry.IsDir() {
				files = append(files, file)
			}
		}
		return nil
	})
	return files, err
}
