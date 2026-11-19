package cmd

import (
	"context"

	"charm.land/fang/v2"
	"github.com/mythicisland/queue/cli/cmd/contexts"
	"github.com/mythicisland/queue/cli/internal/config"
	"github.com/spf13/cobra"
)

// set by goreleaser through -ldflags.
var version = "dev"

func Execute() error {
	var overrides config.Overrides

	root := &cobra.Command{
		Use:   "queue",
		Short: "Manage your queue from the terminal",
	}
	root.PersistentFlags().StringVar(&overrides.Context, "context", "", "context to use instead of the current one")
	root.PersistentFlags().StringVar(&overrides.URL, "url", "", "queue address, overrides the context")
	root.PersistentFlags().StringVar(&overrides.Secret, "secret", "", "auth secret, overrides the context")

	root.AddCommand(contexts.NewCommand())

	return fang.Execute(context.Background(), root, fang.WithVersion(version))
}
