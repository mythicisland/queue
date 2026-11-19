package contexts

import (
	"errors"
	"fmt"

	"charm.land/huh/v2"
	"github.com/mythicisland/queue/cli/internal/config"
	"github.com/mythicisland/queue/cli/internal/output"
	"github.com/mythicisland/queue/cli/internal/prompt"
	"github.com/spf13/cobra"
)

func newAddCommand() *cobra.Command {
	var ctx config.Context

	cmd := &cobra.Command{
		Use:   "add <name>",
		Short: "Add a queue to connect to",
		Example: `  queue context add prod --url queue.example.com:4564 --tls
  queue context add local --url localhost:4564 --secret sup3rS3cr3t`,
		Args: cobra.ExactArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			name := args[0]
			cfg, err := config.Load()
			if err != nil {
				return err
			}
			if _, exists := cfg.Contexts[name]; exists {
				return fmt.Errorf("context %q already exists", name)
			}

			if ctx.Secret == "" {
				secret, err := prompt.Password("Auth secret")
				if errors.Is(err, huh.ErrUserAborted) {
					return nil
				}
				if err != nil {
					return err
				}
				ctx.Secret = secret
			}

			cfg.Contexts[name] = ctx
			if cfg.Current == "" {
				cfg.Current = name
			}
			if err := cfg.Save(); err != nil {
				return err
			}

			w := cmd.OutOrStdout()
			output.Success(w, "Added context %s", name)
			if cfg.Current == name {
				output.Next(w, output.Step{Command: "queue status", Description: "Check that the queue is reachable"})
			} else {
				output.Next(w, output.Step{Command: "queue context use " + name, Description: "Switch to it"})
			}
			return nil
		},
	}
	cmd.Flags().StringVar(&ctx.URL, "url", "", "address of the queue, e.g. queue.example.com:4564")
	cmd.Flags().StringVar(&ctx.Secret, "secret", "", "auth secret, asked for if not set")
	cmd.Flags().BoolVar(&ctx.TLS, "tls", false, "connect with TLS")
	_ = cmd.MarkFlagRequired("url")
	return cmd
}
