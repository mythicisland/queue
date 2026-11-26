package output

import (
	"fmt"
	"io"

	"github.com/mythicisland/queue/cli/internal/manifest"
	"github.com/spf13/cobra"
	"google.golang.org/protobuf/proto"
)

const (
	FormatTable = "table"
	FormatYAML  = manifest.YAML
	FormatJSON  = manifest.JSON
)

func Format(cmd *cobra.Command, fallback string) (string, error) {
	format, _ := cmd.Flags().GetString("output")
	switch format {
	case "":
		return fallback, nil
	case FormatTable, FormatYAML, FormatJSON:
		return format, nil
	default:
		return "", fmt.Errorf("unknown output format %q, use table, yaml or json", format)
	}
}

func Print(w io.Writer, message proto.Message, format string) error {
	data, err := manifest.Encode(message, format)
	if err != nil {
		return err
	}
	_, err = w.Write(data)
	return err
}

func PrintList[T proto.Message](w io.Writer, messages []T, format string) error {
	data, err := manifest.EncodeList(messages, format)
	if err != nil {
		return err
	}
	_, err = w.Write(data)
	return err
}
