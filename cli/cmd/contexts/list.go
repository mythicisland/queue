package contexts

import (
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

			w := cmd.OutOrStdout()
			if len(cfg.Contexts) == 0 {
				output.Info(w, "No contexts yet")
				output.Next(w, output.Step{Command: "queue context add <name> --url <address>", Description: "Add the queue you want to manage"})
				return nil
			}

			rows := make([][]string, 0, len(cfg.Contexts))
			for _, name := range cfg.Names() {
				ctx := cfg.Contexts[name]
				current := ""
				if name == cfg.Current {
					current = output.Accent.Render("●")
				}
				tls := output.Faint.Render("no")
				if ctx.TLS {
					tls = "yes"
				}
				rows = append(rows, []string{current, name, ctx.URL, tls})
			}
			output.Table(w, []string{"", "NAME", "URL", "TLS"}, rows)
			output.Footer(w, output.Count(len(cfg.Contexts), "context"))
			return nil
		},
	}
}
