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
	jsonData, err := yaml.YAMLToJSON(data)
	if err != nil {
		return nil, err
	}

	queueType := &queuev1.QueueType{}
	if err := protojson.Unmarshal(jsonData, queueType); err != nil {
		return nil, fmt.Errorf("invalid queue type: %w", err)
	}
	return queueType, nil
}

func Encode(queueType *queuev1.QueueType, format string) ([]byte, error) {
	jsonData, err := marshalOptions.Marshal(queueType)
	if err != nil {
		return nil, err
	}
	if format == YAML {
		return yaml.JSONToYAML(jsonData)
	}
	return indent(jsonData)
}

func EncodeList(queueTypes []*queuev1.QueueType, format string) ([]byte, error) {
	var out bytes.Buffer
	if format == YAML {
		for i, queueType := range queueTypes {
			if i > 0 {
				out.WriteString("---\n")
			}
			data, err := Encode(queueType, YAML)
			if err != nil {
				return nil, err
			}
			out.Write(data)
		}
		return out.Bytes(), nil
	}

	items := make([]json.RawMessage, 0, len(queueTypes))
	for _, queueType := range queueTypes {
		data, err := marshalOptions.Marshal(queueType)
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
