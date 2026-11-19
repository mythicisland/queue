package contexts

import (
	"github.com/mythicisland/queue/cli/internal/config"
	"github.com/spf13/cobra"
)

func NewCommand() *cobra.Command {
	cmd := &cobra.Command{
		Use:     "context",
		Aliases: []string{"ctx"},
		Short:   "Manage the queues you can connect to",
	}
	cmd.AddCommand(newAddCommand(), newUseCommand(), newListCommand(), newRemoveCommand())
	return cmd
}

func completeNames(_ *cobra.Command, args []string, _ string) ([]string, cobra.ShellCompDirective) {
	if len(args) > 0 {
		return nil, cobra.ShellCompDirectiveNoFileComp
	}
	cfg, err := config.Load()
	if err != nil {
		return nil, cobra.ShellCompDirectiveError
	}
	return cfg.Names(), cobra.ShellCompDirectiveNoFileComp
}
