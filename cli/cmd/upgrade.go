package cmd

import (
	"github.com/mythicisland/queue/cli/internal/output"
	"github.com/mythicisland/queue/cli/internal/release"
	"github.com/spf13/cobra"
)

func newUpgradeCommand() *cobra.Command {
	var target string

	cmd := &cobra.Command{
		Use:   "upgrade",
		Short: "Upgrade the CLI to the latest version",
		Example: `  queue upgrade
  queue upgrade --version 1.0.0-beta.8`,
		Args: cobra.NoArgs,
		RunE: func(cmd *cobra.Command, _ []string) error {
			w := cmd.OutOrStdout()

			tag := release.Tag(target)
			if target == "" {
				latest, err := release.Latest(cmd.Context())
				if err != nil {
					return err
				}
				tag = latest
			}
			if release.Version(tag) == version {
				output.Success(w, "Already on %s", version)
				return nil
			}

			output.Info(w, "Downloading %s", release.Version(tag))
			binary, err := release.Binary(cmd.Context(), tag)
			if err != nil {
				return err
			}
			if err := release.Replace(binary); err != nil {
				return err
			}
			output.Success(w, "Upgraded queue from %s to %s", version, release.Version(tag))
			return nil
		},
	}
	cmd.Flags().StringVar(&target, "version", "", "install this version instead of the latest")
	return cmd
}
