package wizard

import (
	"errors"
	"time"

	"charm.land/huh/v2"
	"github.com/mythicisland/queue/cli/internal/manifest"
	"github.com/mythicisland/queue/cli/internal/prompt"
)

func QueueType() (manifest.Spec, error) {
	var spec manifest.Spec
	var err error

	spec.Name, err = prompt.Input("Name", "bedwars-4x4", required)
	if err != nil {
		return spec, err
	}

	spec.Group, err = prompt.Input("Server group", spec.Name, nil)
	if err != nil {
		return spec, err
	}
	if spec.Group == "" {
		spec.Group = spec.Name
	}

	spec.Shared, err = prompt.Select("Servers",
		huh.NewOption("Exclusive, one match per server", false),
		huh.NewOption("Shared, several matches per server", true),
	)
	if err != nil {
		return spec, err
	}

	withTeams, err := prompt.Select("Teams",
		huh.NewOption("Everyone for themselves", false),
		huh.NewOption("Teams", true),
	)
	if err != nil {
		return spec, err
	}

	if withTeams {
		teams, err := prompt.Input("Teams", "2x4", validTeams)
		if err != nil {
			return spec, err
		}
		spec.TeamCount, spec.TeamSize, _ = manifest.ParseTeams(teams)
	} else {
		players, err := prompt.Input("Players", "2-16", validPlayers)
		if err != nil {
			return spec, err
		}
		spec.MinPlayers, spec.MaxPlayers, _ = manifest.ParsePlayers(players)
	}

	maxWait, err := prompt.Input("Max wait", "5m, empty to search forever", validDuration)
	if err != nil {
		return spec, err
	}
	if maxWait != "" {
		spec.MaxWait, _ = time.ParseDuration(maxWait)
	}
	return spec, nil
}

func required(value string) error {
	if value == "" {
		return errors.New("required")
	}
	return nil
}

func validPlayers(value string) error {
	_, _, err := manifest.ParsePlayers(value)
	return err
}

func validTeams(value string) error {
	_, _, err := manifest.ParseTeams(value)
	return err
}

func validDuration(value string) error {
	if value == "" {
		return nil
	}
	if _, err := time.ParseDuration(value); err != nil {
		return errors.New("must look like 30s, 5m or 1h")
	}
	return nil
}
