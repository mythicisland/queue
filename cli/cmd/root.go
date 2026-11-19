package cmd

import (
	"context"

	"charm.land/fang/v2"
	"github.com/mythicisland/queue/cli/cmd/contexts"
	"github.com/mythicisland/queue/cli/cmd/types"
	"github.com/spf13/cobra"
)

// set by goreleaser through -ldflags.
var version = "dev"

func Execute() error {
	root := &cobra.Command{
		Use:   "queue",
		Short: "Manage your queue from the terminal",
	}
	flags := root.PersistentFlags()
	flags.String("context", "", "context to use instead of the current one")
	flags.String("url", "", "queue address, overrides the context")
	flags.String("secret", "", "auth secret, overrides the context")
	flags.StringP("output", "o", "", "output format: table, yaml or json")

	root.AddCommand(contexts.NewCommand(), types.NewCommand())

	return fang.Execute(context.Background(), root, fang.WithVersion(version))
}
