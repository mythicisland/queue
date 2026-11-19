package wizard

import (
	"errors"
	"time"

	"charm.land/huh/v2"
	"github.com/mythicisland/queue/cli/internal/manifest"
)

func QueueType() (manifest.Spec, error) {
	var spec manifest.Spec
	var withTeams bool
	var players, teams, maxWait string

	form := huh.NewForm(
		huh.NewGroup(
			huh.NewInput().
				Title("Name").
				Placeholder("bedwars-4x4").
				Validate(required).
				Value(&spec.Name),
			huh.NewInput().
				Title("Server group").
				Description("The SimpleCloud group the matches run on").
				Validate(required).
				Value(&spec.Group),
			huh.NewSelect[bool]().
				Title("Servers").
				Options(
					huh.NewOption("Exclusive, one match per server", false),
					huh.NewOption("Shared, several matches per server", true),
				).
				Value(&spec.Shared),
			huh.NewSelect[bool]().
				Title("Teams").
				Options(
					huh.NewOption("Everyone for themselves", false),
					huh.NewOption("Teams", true),
				).
				Value(&withTeams),
		),
		huh.NewGroup(
			huh.NewInput().
				Title("Players").
				Description("Minimum and maximum players per match").
				Placeholder("2-16").
				Validate(validPlayers).
				Value(&players),
		).WithHideFunc(func() bool { return withTeams }),
		huh.NewGroup(
			huh.NewInput().
				Title("Teams").
				Description("Number of teams x players per team").
				Placeholder("2x4").
				Validate(validTeams).
				Value(&teams),
		).WithHideFunc(func() bool { return !withTeams }),
		huh.NewGroup(
			huh.NewInput().
				Title("Max wait").
				Description("How long a ticket searches before it is removed, empty to search forever").
				Placeholder("5m").
				Validate(validDuration).
				Value(&maxWait),
		),
	)
	if err := form.Run(); err != nil {
		return spec, err
	}

	if withTeams {
		spec.TeamCount, spec.TeamSize, _ = manifest.ParseTeams(teams)
	} else {
		spec.MinPlayers, spec.MaxPlayers, _ = manifest.ParsePlayers(players)
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
