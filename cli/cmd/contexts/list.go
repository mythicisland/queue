package contexts

import (
	"fmt"
	"strconv"

	"github.com/mythicisland/queue/cli/internal/config"
	"github.com/mythicisland/queue/cli/internal/output"
	"github.com/spf13/cobra"
)

func newListCommand() *cobra.Command {
	return &cobra.Command{
		Use:     "list",
		Aliases: []string{"ls"},
		Short:   "List all queues",
		Args:    cobra.NoArgs,
		RunE: func(cmd *cobra.Command, _ []string) error {
			cfg, err := config.Load()
			if err != nil {
				return err
			}
			if len(cfg.Contexts) == 0 {
				fmt.Fprintln(cmd.OutOrStdout(), "No contexts yet, add one with `queue context add`")
				return nil
			}

			rows := make([][]string, 0, len(cfg.Contexts))
			for _, name := range cfg.Names() {
				ctx := cfg.Contexts[name]
				current := ""
				if name == cfg.Current {
					current = "*"
				}
				rows = append(rows, []string{current, name, ctx.URL, strconv.FormatBool(ctx.TLS)})
			}
			output.Table([]string{"", "NAME", "URL", "TLS"}, rows)
			return nil
		},
	}
}
