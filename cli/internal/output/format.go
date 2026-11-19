package output

import (
	"fmt"

	"github.com/mythicisland/queue/cli/internal/manifest"
	"github.com/spf13/cobra"
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
