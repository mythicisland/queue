package cmd

import (
	"fmt"
	"strings"

	"charm.land/lipgloss/v2"
	"github.com/mythicisland/queue/cli/internal/client"
	"github.com/mythicisland/queue/cli/internal/manifest"
	"github.com/mythicisland/queue/cli/internal/output"
	"github.com/mythicisland/queue/cli/internal/plan"
	"github.com/spf13/cobra"
)

type actionLook struct {
	symbol  string
	style   lipgloss.Style
	done    string
	planned string
}

var actions = map[plan.Action]actionLook{
	plan.Create:    {"+", output.Green, "Created", "Would create"},
	plan.Update:    {"~", output.Yellow, "Updated", "Would update"},
	plan.Delete:    {"-", output.Red, "Deleted", "Would delete"},
	plan.Unchanged: {"=", output.Faint, "Unchanged", "Unchanged"},
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

			w := cmd.OutOrStdout()
			counts := map[plan.Action]int{}
			for _, change := range changes {
				if !dryRun {
					if err := plan.Apply(cmd.Context(), c, change); err != nil {
						return err
					}
				}
				counts[change.Action]++
				look := actions[change.Action]
				verb := look.done
				if dryRun {
					verb = look.planned
				}
				lipgloss.Fprintln(w, look.style.Render(look.symbol)+" "+verb+" queue type "+look.style.Render(change.Name()))
			}

			var summary []string
			for _, action := range []plan.Action{plan.Create, plan.Update, plan.Delete, plan.Unchanged} {
				if counts[action] > 0 {
					summary = append(summary, fmt.Sprintf("%d %s", counts[action], strings.ToLower(actions[action].done)))
				}
			}
			if len(summary) == 0 {
				output.Info(w, "No queue types in %s", file)
				return nil
			}
			output.Footer(w, summary...)
			return nil
		},
	}
	cmd.Flags().StringVarP(&file, "file", "f", "", "file or directory with queue types, - for stdin")
	cmd.Flags().BoolVar(&prune, "prune", false, "delete queue types that have no file")
	cmd.Flags().BoolVar(&dryRun, "dry-run", false, "only check the queue types without changing anything")
	_ = cmd.MarkFlagRequired("file")
	return cmd
}
