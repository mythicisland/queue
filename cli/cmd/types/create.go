package types

import (
	"errors"
	"time"

	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"charm.land/huh/v2"
	"github.com/mythicisland/queue/cli/internal/client"
	"github.com/mythicisland/queue/cli/internal/manifest"
	"github.com/mythicisland/queue/cli/internal/output"
	"github.com/mythicisland/queue/cli/internal/wizard"
	"github.com/spf13/cobra"
)

type createFlags struct {
	file    string
	group   string
	players string
	teams   string
	shared  bool
	maxWait time.Duration
	dryRun  bool
}

func newCreateCommand() *cobra.Command {
	var flags createFlags

	cmd := &cobra.Command{
		Use:   "create [name]",
		Short: "Create a queue type",
		Long: `Create a queue type from flags, from a file or, without any arguments, step by step.

Flags cover the basics. For rules, filters and match quality, use a file or edit the queue type afterwards.`,
		Example: `  queue types create
  queue types create ffa --players 2-16 --max-wait 5m
  queue types create bedwars-4x4 --group bedwars --teams 4x4 --shared
  queue types create -f ranked.yaml`,
		Args: cobra.MaximumNArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			queueType, err := queueTypeFrom(flags, args)
			if errors.Is(err, huh.ErrUserAborted) {
				return nil
			}
			if err != nil {
				return err
			}

			c, err := client.Connect(cmd)
			if err != nil {
				return err
			}
			defer c.Close()

			req := queuev1.CreateQueueTypeRequest_builder{QueueType: queueType, ValidateOnly: flags.dryRun}.Build()
			res, err := c.Types.CreateQueueType(cmd.Context(), req)
			if err != nil {
				return err
			}

			w := cmd.OutOrStdout()
			name := res.GetQueueType().GetName()
			if flags.dryRun {
				output.Success(w, "Queue type %s is valid", name)
				return nil
			}
			output.Success(w, "Created queue type %s", name)
			output.Next(w,
				output.Step{Command: "queue types get " + name, Description: "Show the full config with all defaults"},
				output.Step{Command: "queue types edit " + name, Description: "Add rules, filters or match quality"},
			)
			return nil
		},
	}
	cmd.Flags().StringVarP(&flags.file, "file", "f", "", "YAML or JSON file with the queue type, - for stdin")
	cmd.Flags().StringVar(&flags.group, "group", "", "server group of the matches (default: the name)")
	cmd.Flags().StringVar(&flags.players, "players", "", "players per match, e.g. 2-16 or 8")
	cmd.Flags().StringVar(&flags.teams, "teams", "", "teams per match, e.g. 2x4")
	cmd.Flags().BoolVar(&flags.shared, "shared", false, "run several matches per server")
	cmd.Flags().DurationVar(&flags.maxWait, "max-wait", 0, "how long a ticket searches before it is removed")
	cmd.Flags().BoolVar(&flags.dryRun, "dry-run", false, "only check the queue type without creating it")
	cmd.MarkFlagsMutuallyExclusive("players", "teams")
	cmd.MarkFlagsMutuallyExclusive("file", "players")
	cmd.MarkFlagsMutuallyExclusive("file", "teams")
	return cmd
}

func queueTypeFrom(flags createFlags, args []string) (*queuev1.QueueType, error) {
	if flags.file != "" {
		data, err := manifest.Read(flags.file)
		if err != nil {
			return nil, err
		}
		queueType, err := manifest.Decode(data)
		if err != nil {
			return nil, err
		}
		if len(args) == 1 {
			queueType.SetName(args[0])
		}
		return queueType, nil
	}

	if len(args) == 0 {
		spec, err := wizard.QueueType()
		if err != nil {
			return nil, err
		}
		return spec.QueueType(), nil
	}

	spec := manifest.Spec{
		Name:    args[0],
		Group:   flags.group,
		Shared:  flags.shared,
		MaxWait: flags.maxWait,
	}
	if spec.Group == "" {
		spec.Group = spec.Name
	}

	var err error
	switch {
	case flags.teams != "":
		spec.TeamCount, spec.TeamSize, err = manifest.ParseTeams(flags.teams)
	case flags.players != "":
		spec.MinPlayers, spec.MaxPlayers, err = manifest.ParsePlayers(flags.players)
	default:
		err = errors.New("set --players or --teams")
	}
	if err != nil {
		return nil, err
	}
	return spec.QueueType(), nil
}
