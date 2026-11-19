package contexts

import (
	"fmt"

	"github.com/mythicisland/queue/cli/internal/config"
	"github.com/mythicisland/queue/cli/internal/output"
	"github.com/spf13/cobra"
)

func newUseCommand() *cobra.Command {
	return &cobra.Command{
		Use:               "use <name>",
		Short:             "Switch to another queue",
		Args:              cobra.ExactArgs(1),
		ValidArgsFunction: completeNames,
		RunE: func(cmd *cobra.Command, args []string) error {
			name := args[0]
			cfg, err := config.Load()
			if err != nil {
				return err
			}
			if _, exists := cfg.Contexts[name]; !exists {
				return fmt.Errorf("context %q does not exist", name)
			}

			cfg.Current = name
			if err := cfg.Save(); err != nil {
				return err
			}

			output.Success(cmd.OutOrStdout(), "Switched to context %s", name)
			return nil
		},
	}
}
