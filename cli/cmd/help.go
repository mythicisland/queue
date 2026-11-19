package cmd

import (
	"strings"

	"charm.land/lipgloss/v2"
	"github.com/mythicisland/queue/cli/internal/output"
	"github.com/spf13/cobra"
	"github.com/spf13/pflag"
)

func help(cmd *cobra.Command, _ []string) {
	var b strings.Builder

	description := cmd.Long
	if description == "" {
		description = cmd.Short
	}
	b.WriteString(description + "\n")

	usage := cmd.UseLine()
	if cmd.HasAvailableSubCommands() && !cmd.Runnable() {
		usage = cmd.CommandPath() + " <command>"
	}
	section(&b, "Usage", "  "+usage)

	if len(cmd.Aliases) > 0 {
		section(&b, "Aliases", "  "+strings.Join(cmd.Aliases, ", "))
	}

	for _, group := range cmd.Groups() {
		commandSection(&b, group.Title, cmd, group.ID)
	}
	commandSection(&b, "Commands", cmd, "")

	if cmd.Example != "" {
		section(&b, "Examples", output.Accent.Render(cmd.Example))
	}

	if cmd.HasParent() {
		if usages := flagUsages(cmd.LocalNonPersistentFlags()); usages != "" {
			section(&b, "Flags", usages)
		}
		if names := flagNames(cmd.InheritedFlags()); names != "" {
			section(&b, "Global flags", "  "+output.Faint.Render(names))
		}
	} else {
		section(&b, "Global flags", flagUsages(cmd.PersistentFlags()))
	}

	if cmd.HasAvailableSubCommands() {
		b.WriteString("\n" + output.Faint.Render("Run `"+cmd.CommandPath()+" <command> --help` for more about a command.") + "\n")
	}
	lipgloss.Fprint(cmd.OutOrStdout(), b.String())
}

func section(b *strings.Builder, title, body string) {
	b.WriteString("\n" + output.Heading.Render(title) + "\n" + strings.TrimRight(body, "\n") + "\n")
}

func commandSection(b *strings.Builder, title string, cmd *cobra.Command, group string) {
	var commands []*cobra.Command
	for _, command := range cmd.Commands() {
		if command.GroupID == group && (command.IsAvailableCommand() || command.Name() == "help") {
			commands = append(commands, command)
		}
	}
	if len(commands) == 0 {
		return
	}

	width := 0
	for _, command := range commands {
		width = max(width, len(command.Name()))
	}
	var lines []string
	for _, command := range commands {
		padding := strings.Repeat(" ", width-len(command.Name())+3)
		lines = append(lines, "  "+output.Accent.Render(command.Name())+padding+command.Short)
	}
	section(b, title, strings.Join(lines, "\n"))
}

func flagUsages(flags *pflag.FlagSet) string {
	return visible(flags).FlagUsages()
}

func flagNames(flags *pflag.FlagSet) string {
	var names []string
	visible(flags).VisitAll(func(flag *pflag.Flag) {
		if flag.Shorthand != "" {
			names = append(names, "-"+flag.Shorthand+"/--"+flag.Name)
		} else {
			names = append(names, "--"+flag.Name)
		}
	})
	return strings.Join(names, ", ")
}

func visible(flags *pflag.FlagSet) *pflag.FlagSet {
	result := pflag.NewFlagSet("", pflag.ContinueOnError)
	flags.VisitAll(func(flag *pflag.Flag) {
		if !flag.Hidden && flag.Name != "help" {
			result.AddFlag(flag)
		}
	})
	return result
}
