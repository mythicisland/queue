package cmd

import (
	"context"
	"os"
	"os/signal"

	"github.com/mythicisland/queue/cli/cmd/contexts"
	"github.com/mythicisland/queue/cli/cmd/matches"
	"github.com/mythicisland/queue/cli/cmd/tickets"
	"github.com/mythicisland/queue/cli/cmd/types"
	"github.com/spf13/cobra"
)

// set by goreleaser
var version = "dev"

func Execute() error {
	root := &cobra.Command{
		Use:           "queue",
		Short:         "Manage your queue from the terminal",
		Version:       version,
		SilenceUsage:  true,
		SilenceErrors: true,
	}
	root.SetVersionTemplate("queue {{.Version}}\n")
	root.SetHelpFunc(help)

	flags := root.PersistentFlags()
	flags.String("context", "", "context to use instead of the current one")
	flags.String("url", "", "queue address, overrides the context")
	flags.String("secret", "", "auth secret, overrides the context")
	flags.StringP("output", "o", "", "output format: table, yaml or json")

	root.AddGroup(
		&cobra.Group{ID: "start", Title: "Get started"},
		&cobra.Group{ID: "types", Title: "Queue types"},
		&cobra.Group{ID: "debug", Title: "Debugging"},
		&cobra.Group{ID: "cli", Title: "CLI"},
	)
	addToGroup(root, "start", contexts.NewCommand(), newStatusCommand())
	addToGroup(root, "types", types.NewCommand(), newApplyCommand(), newDiffCommand())
	addToGroup(root, "debug", tickets.NewCommand(), matches.NewCommand())
	addToGroup(root, "cli", newUpgradeCommand())
	root.SetHelpCommandGroupID("cli")
	root.SetCompletionCommandGroupID("cli")

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt)
	defer stop()
	return root.ExecuteContext(ctx)
}

func addToGroup(root *cobra.Command, group string, commands ...*cobra.Command) {
	for _, command := range commands {
		command.GroupID = group
		root.AddCommand(command)
	}
}
