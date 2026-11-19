package cmd

import (
	"context"

	"charm.land/fang/v2"
	"github.com/spf13/cobra"
)

// Set by goreleaser through -ldflags.
var version = "dev"

func Execute() error {
	root := &cobra.Command{
		Use:   "queue",
		Short: "Manage your queue from the terminal",
	}

	return fang.Execute(context.Background(), root, fang.WithVersion(version))
}
