package release

import (
	"archive/tar"
	"archive/zip"
	"bufio"
	"bytes"
	"compress/gzip"
	"context"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"runtime"
	"strings"
	"time"
)

const (
	repo      = "mythicisland/queue"
	checksums = "queue-cli_checksums.txt"
)

var httpClient = &http.Client{Timeout: 2 * time.Minute}

type githubRelease struct {
	TagName string `json:"tag_name"`
	Draft   bool   `json:"draft"`
	Assets  []struct {
		Name string `json:"name"`
	} `json:"assets"`
}

func Tag(version string) string {
	return "v" + strings.TrimPrefix(version, "v")
}

func Version(tag string) string {
	return strings.TrimPrefix(tag, "v")
}

func Latest(ctx context.Context) (string, error) {
	data, err := get(ctx, "https://api.github.com/repos/"+repo+"/releases?per_page=20")
	if err != nil {
		return "", err
	}
	var releases []githubRelease
	if err := json.Unmarshal(data, &releases); err != nil {
		return "", fmt.Errorf("read releases: %w", err)
	}
	for _, release := range releases {
		if release.Draft {
			continue
		}
		for _, asset := range release.Assets {
			if asset.Name == checksums {
				return release.TagName, nil
			}
		}
	}
	return "", errors.New("no release of the CLI found")
}

func Binary(ctx context.Context, tag string) ([]byte, error) {
	archive := archiveName()
	base := "https://github.com/" + repo + "/releases/download/" + tag + "/"

	sums, err := get(ctx, base+checksums)
	if errors.Is(err, errNotFound) {
		return nil, fmt.Errorf("version %s does not exist", Version(tag))
	}
	if err != nil {
		return nil, err
	}
	want, err := checksumOf(sums, archive)
	if err != nil {
		return nil, err
	}

	data, err := get(ctx, base+archive)
	if errors.Is(err, errNotFound) {
		return nil, fmt.Errorf("version %s has no build for %s/%s", Version(tag), runtime.GOOS, runtime.GOARCH)
	}
	if err != nil {
		return nil, err
	}
	sum := sha256.Sum256(data)
	if hex.EncodeToString(sum[:]) != want {
		return nil, fmt.Errorf("checksum of %s does not match, try again", archive)
	}

	if runtime.GOOS == "windows" {
		return fromZip(data, "queue.exe")
	}
	return fromTarGz(data, "queue")
}

func archiveName() string {
	if runtime.GOOS == "windows" {
		return fmt.Sprintf("queue-cli_%s_%s.zip", runtime.GOOS, runtime.GOARCH)
	}
	return fmt.Sprintf("queue-cli_%s_%s.tar.gz", runtime.GOOS, runtime.GOARCH)
}

func checksumOf(sums []byte, name string) (string, error) {
	scanner := bufio.NewScanner(bytes.NewReader(sums))
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) == 2 && fields[1] == name {
			return fields[0], nil
		}
	}
	return "", fmt.Errorf("no checksum for %s", name)
}

func fromTarGz(data []byte, name string) ([]byte, error) {
	gz, err := gzip.NewReader(bytes.NewReader(data))
	if err != nil {
		return nil, err
	}
	defer gz.Close()

	reader := tar.NewReader(gz)
	for {
		header, err := reader.Next()
		if errors.Is(err, io.EOF) {
			return nil, fmt.Errorf("%s is missing in the archive", name)
		}
		if err != nil {
			return nil, err
		}
		if header.Name == name {
			return io.ReadAll(reader)
		}
	}
}

func fromZip(data []byte, name string) ([]byte, error) {
	reader, err := zip.NewReader(bytes.NewReader(data), int64(len(data)))
	if err != nil {
		return nil, err
	}
	file, err := reader.Open(name)
	if err != nil {
		return nil, fmt.Errorf("%s is missing in the archive", name)
	}
	defer file.Close()
	return io.ReadAll(file)
}

var errNotFound = errors.New("not found")

func get(ctx context.Context, url string) ([]byte, error) {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("User-Agent", "mythicisland/queue")

	res, err := httpClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer res.Body.Close()

	if res.StatusCode == http.StatusNotFound {
		return nil, errNotFound
	}
	if res.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("GET %s: %s", url, res.Status)
	}
	return io.ReadAll(res.Body)
}
