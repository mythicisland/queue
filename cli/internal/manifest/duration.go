package manifest

import (
	"strconv"
	"strings"
	"time"

	"github.com/goccy/go-yaml"
)

var durationKeys = map[string]bool{
	"allocation_timeout": true,
	"accept_timeout":     true,
	"fill_time":          true,
	"countdown":          true,
	"max_wait":           true,
	"relax_after":        true,
	"every":              true,
}

func convertDurations(value any, convert func(time.Duration) string) any {
	switch typed := value.(type) {
	case yaml.MapSlice:
		for i, item := range typed {
			key, _ := item.Key.(string)
			text, isText := item.Value.(string)
			if !durationKeys[key] || !isText {
				typed[i].Value = convertDurations(item.Value, convert)
				continue
			}
			duration, err := time.ParseDuration(strings.TrimSpace(text))
			if err == nil {
				typed[i].Value = convert(duration)
			}
		}
	case []any:
		for i, item := range typed {
			typed[i] = convertDurations(item, convert)
		}
	}
	return value
}

func protoDuration(duration time.Duration) string {
	return strconv.FormatFloat(duration.Seconds(), 'f', -1, 64) + "s"
}

func readableDuration(duration time.Duration) string {
	text := duration.String()
	if strings.HasSuffix(text, "m0s") {
		text = strings.TrimSuffix(text, "0s")
	}
	if strings.HasSuffix(text, "h0m") {
		text = strings.TrimSuffix(text, "0m")
	}
	return text
}
