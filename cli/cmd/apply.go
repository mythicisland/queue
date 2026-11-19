package cmd

import (
	"charm.land/lipgloss/v2"
	"github.com/mythicisland/queue/cli/internal/client"
	"github.com/mythicisland/queue/cli/internal/manifest"
	"github.com/mythicisland/queue/cli/internal/plan"
	"github.com/spf13/cobra"
)

var actionStyles = map[plan.Action]lipgloss.Style{
	plan.Create:    lipgloss.NewStyle().Foreground(lipgloss.Green),
	plan.Update:    lipgloss.NewStyle().Foreground(lipgloss.Yellow),
	plan.Delete:    lipgloss.NewStyle().Foreground(lipgloss.Red),
	plan.Unchanged: lipgloss.NewStyle().Faint(true),
}

var pastTense = map[plan.Action]string{
	plan.Create:    "created",
	plan.Update:    "updated",
	plan.Delete:    "deleted",
	plan.Unchanged: "unchanged",
}

func newApplyCommand() *cobra.Command {
	var file string
	var prune, dryRun bool

	cmd := &cobra.Command{
		Use:   "apply",
		Short: "Create or update queue types from files",
		Long: `Create or update all queue types in a file or directory. Nothing is changed if one of them is invalid.

With --prune, queue types without a file are deleted, so the files are the only source of truth.`,
		Example: `  queue apply -f types/
  queue apply -f types/ --prune
  queue apply -f types/ --dry-run`,
		Args: cobra.NoArgs,
		RunE: func(cmd *cobra.Command, _ []string) error {
			wanted, err := manifest.Load(file)
			if err != nil {
				return err
			}

			c, err := client.Connect(cmd)
			if err != nil {
				return err
			}
			defer c.Close()

			changes, err := plan.Make(cmd.Context(), c, wanted, prune)
			if err != nil {
				return err
			}

			suffix := ""
			if dryRun {
				suffix = " (dry run)"
			}
			for _, change := range changes {
				if !dryRun {
					if err := plan.Apply(cmd.Context(), c, change); err != nil {
						return err
					}
				}
				result := actionStyles[change.Action].Render(pastTense[change.Action])
				lipgloss.Fprintln(cmd.OutOrStdout(), "queue type "+change.Name()+" "+result+suffix)
			}
			return nil
		},
	}
	cmd.Flags().StringVarP(&file, "file", "f", "", "file or directory with queue types, - for stdin")
	cmd.Flags().BoolVar(&prune, "prune", false, "delete queue types that have no file")
	cmd.Flags().BoolVar(&dryRun, "dry-run", false, "only check the queue types without changing anything")
	_ = cmd.MarkFlagRequired("file")
	return cmd
}
