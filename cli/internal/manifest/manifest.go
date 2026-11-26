package manifest

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"os"

	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"github.com/goccy/go-yaml"
	"google.golang.org/protobuf/encoding/protojson"
	"google.golang.org/protobuf/proto"
)

const (
	YAML = "yaml"
	JSON = "json"
)

var marshalOptions = protojson.MarshalOptions{UseProtoNames: true}

func Read(path string) ([]byte, error) {
	if path == "-" {
		return io.ReadAll(os.Stdin)
	}
	return os.ReadFile(path)
}

func Decode(data []byte) (*queuev1.QueueType, error) {
	var document any
	if err := yaml.UnmarshalWithOptions(data, &document, yaml.UseOrderedMap()); err != nil {
		return nil, err
	}
	jsonData, err := yaml.MarshalWithOptions(convertDurations(document, protoDuration), yaml.JSON())
	if err != nil {
		return nil, err
	}

	queueType := &queuev1.QueueType{}
	if err := protojson.Unmarshal(jsonData, queueType); err != nil {
		return nil, fmt.Errorf("invalid queue type: %w", err)
	}
	return queueType, nil
}

func Encode(message proto.Message, format string) ([]byte, error) {
	jsonData, err := marshalOptions.Marshal(message)
	if err != nil {
		return nil, err
	}
	if format != YAML {
		return indent(jsonData)
	}

	var document any
	if err := yaml.UnmarshalWithOptions(jsonData, &document, yaml.UseOrderedMap()); err != nil {
		return nil, err
	}
	return yaml.Marshal(convertDurations(document, readableDuration))
}

func EncodeList[T proto.Message](messages []T, format string) ([]byte, error) {
	var out bytes.Buffer
	if format == YAML {
		for i, message := range messages {
			if i > 0 {
				out.WriteString("---\n")
			}
			data, err := Encode(message, YAML)
			if err != nil {
				return nil, err
			}
			out.Write(data)
		}
		return out.Bytes(), nil
	}

	items := make([]json.RawMessage, 0, len(messages))
	for _, message := range messages {
		data, err := marshalOptions.Marshal(message)
		if err != nil {
			return nil, err
		}
		items = append(items, data)
	}
	data, err := json.Marshal(items)
	if err != nil {
		return nil, err
	}
	return indent(data)
}

func indent(data []byte) ([]byte, error) {
	var out bytes.Buffer
	if err := json.Indent(&out, data, "", "  "); err != nil {
		return nil, err
	}
	out.WriteByte('\n')
	return out.Bytes(), nil
}
