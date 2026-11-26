package wizard

import (
	"errors"
	"strings"
	"time"

	"charm.land/huh/v2"
	"github.com/mythicisland/queue/cli/internal/manifest"
	"github.com/mythicisland/queue/cli/internal/output"
	"github.com/mythicisland/queue/cli/internal/prompt"
)

type answers struct {
	Name      string
	Group     string
	Shared    bool
	WithTeams bool
	Players   string
	Teams     string
	MaxWait   string
	Confirmed bool
}

func QueueType() (manifest.Spec, error) {
	a := &answers{Confirmed: true}

	form := huh.NewForm(
		huh.NewGroup(
			huh.NewInput().
				Title("Name").
				Description("Plugins and players use it to join the queue.").
				Placeholder("bedwars-4x4").
				Prompt("› ").
				Validate(required).
				Value(&a.Name),
			huh.NewInput().
				Title("Server group").
				Description("The SimpleCloud group the matches run on.").
				PlaceholderFunc(a.groupPlaceholder, &a.Name).
				Prompt("› ").
				Value(&a.Group),
		).Title("Create a queue type").Description("Step 1 of 4 · Basics"),

		huh.NewGroup(
			huh.NewSelect[bool]().
				Title("Servers").
				Options(
					huh.NewOption("Exclusive  one match per server", false),
					huh.NewOption("Shared     several matches per server", true),
				).
				Value(&a.Shared),
			huh.NewSelect[bool]().
				Title("Format").
				Options(
					huh.NewOption("Free for all", false),
					huh.NewOption("Teams", true),
				).
				Value(&a.WithTeams),
		).Title("Create a queue type").Description("Step 2 of 4 · Matches"),

		huh.NewGroup(
			huh.NewInput().
				Title("Teams").
				Description("Number of teams × players per team, e.g. 2x4.").
				Placeholder("2x4").
				Prompt("› ").
				Validate(validTeams).
				Value(&a.Teams),
		).Title("Create a queue type").Description("Step 3 of 4 · Players").WithHideFunc(a.isFreeForAll),

		huh.NewGroup(
			huh.NewInput().
				Title("Players").
				Description("Players per match, e.g. 2-16 or exactly 8.").
				Placeholder("2-16").
				Prompt("› ").
				Validate(validPlayers).
				Value(&a.Players),
		).Title("Create a queue type").Description("Step 3 of 4 · Players").WithHideFunc(a.hasTeams),

		huh.NewGroup(
			huh.NewInput().
				Title("Max wait").
				Description("Tickets are removed after searching this long. Leave it empty to search forever.").
				Placeholder("5m").
				Prompt("› ").
				Validate(validDuration).
				Value(&a.MaxWait),
			huh.NewNote().
				Title("Summary").
				DescriptionFunc(a.summary, a),
			huh.NewConfirm().
				Title("Create this queue type?").
				Affirmative("Create").
				Negative("Cancel").
				Value(&a.Confirmed),
		).Title("Create a queue type").Description("Step 4 of 4 · Review"),
	).WithTheme(prompt.Theme).WithShowHelp(true)

	if err := form.Run(); err != nil {
		return manifest.Spec{}, err
	}
	if !a.Confirmed {
		return manifest.Spec{}, huh.ErrUserAborted
	}
	return a.spec(), nil
}

func (a *answers) spec() manifest.Spec {
	spec := manifest.Spec{Name: a.Name, Group: a.serverGroup(), Shared: a.Shared}
	if a.WithTeams {
		spec.TeamCount, spec.TeamSize, _ = manifest.ParseTeams(a.Teams)
	} else {
		spec.MinPlayers, spec.MaxPlayers, _ = manifest.ParsePlayers(a.Players)
	}
	if a.MaxWait != "" {
		spec.MaxWait, _ = time.ParseDuration(a.MaxWait)
	}
	return spec
}

func (a *answers) summary() string {
	servers := "exclusive"
	if a.Shared {
		servers = "shared"
	}
	players := "players " + a.Players
	if a.WithTeams {
		players = "teams " + a.Teams
	}
	maxWait := "forever"
	if a.MaxWait != "" {
		maxWait = a.MaxWait
	}

	rows := [][2]string{
		{"Name", a.Name},
		{"Group", a.serverGroup()},
		{"Servers", servers},
		{"Match", players},
		{"Max wait", maxWait},
	}
	var b strings.Builder
	for _, row := range rows {
		b.WriteString(output.Faint.Render(padRight(row[0], 10)) + row[1] + "\n")
	}
	return strings.TrimSuffix(b.String(), "\n")
}

func (a *answers) serverGroup() string {
	if a.Group == "" {
		return a.Name
	}
	return a.Group
}

func (a *answers) groupPlaceholder() string {
	if a.Name == "" {
		return "same as the name"
	}
	return a.Name
}

func (a *answers) hasTeams() bool {
	return a.WithTeams
}

func (a *answers) isFreeForAll() bool {
	return !a.WithTeams
}

func padRight(value string, width int) string {
	return value + strings.Repeat(" ", max(1, width-len(value)))
}

func required(value string) error {
	if strings.TrimSpace(value) == "" {
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
