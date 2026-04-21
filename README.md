Simple setup to use Claude Code on your local machine in a devcontainer. The primary target for this template
are projects written in Vaadin with Spring Boot, but it should also work for other types of projects. 

If you are not building a Vaadin project, you may need to modify the agents.

Inspired by [Claude Code's original devcontainer setup](https://github.com/anthropics/claude-code/tree/main/.devcontainer). Most important change is, that the firewall is not active and claude has priviliged rights inside the container.

## Disclaimer / Warning
Before you do anything with the files provided here, be aware, that devcontainers may have unknown (or known) bugs, that may allow
Claude to break out and do unwanted things with your host machine. Also be aware, that the given setup has no firewall rules enabled, so
Claude can access the internet and it has sudo rights inside the devcontainer.

**Everything you do you do on your own risk, I do not give any guarantees
and do not take any responsibility for what you do with this template / repo content.** 


## How to get
### Github template
Fork this repo, mark it as a template in the project settings and create a new repo based on it. 

### Integrate into an existing project
Copy the following things into your project root:
* .claude
* .devcontainer
* .mcp.json

Regarding the specific files and what they are used for, feel free to google or use an AI to explain :)

## How to use
You can either use the devcontainer in an IDE, that supports them (like VSCode) or run it in the shell/terminal. 

### First time setup
When you open the shell the first time for a new project, Claude will ask you about your login. When it shows an URL to open,
copy and paste that URL instead of clicking it, as it may not work correctly otherwise. The resulting passkey must then be copied
back from the browser into the shell. 

Subsequent usages of the shell in this project will then not require another login.

### Shell shortcuts
Here are some useful shortcuts, that can be added to allow easier setup and access:

```shell

# ----------------------------------------------
# Claude DevContainer Shortcuts
# ----------------------------------------------

# 1. Startup and run in background
alias claude-up="devcontainer up --workspace-folder ."

# 2. Open shell
alias claude-shell="devcontainer exec --workspace-folder . zsh"

# 3. Soft-Rebuild (only devcontainer.json changes)
alias claude-update="devcontainer up --workspace-folder . --remove-existing-container"

# 4. Hard-Rebuild (on Dockerfile changes - takes longer)
alias claude-rebuild="devcontainer build --workspace-folder ."

# 4. Hard-Rebuild without cache (on Dockerfile changes - takes even longer ;)
alias claude-rebuild-full="devcontainer build --workspace-folder . --no-cache"


# 5. Smart Stop function
# Finds and stops the container "owned" by the current folder
claude-stop() {
    local container_id=$(docker ps -q --filter "label=devcontainer.local_folder=$PWD")

    if [ -z "$container_id" ]; then
        echo "❌ No running dev container found for this folder / project."
    else
        echo "🛑 Stopping container $container_id ..."
        docker stop $container_id
        echo "✅ Done."
    fi
}

# 6. Clean up - removes docker container and image
claude-clean() {
    # 1. Find container (including stopped ones via -a)
    local container_id=$(docker ps -a -q --filter "label=devcontainer.local_folder=$PWD")

    if [ -z "$container_id" ]; then
        echo "⚠️  No container (running or stopped) found for this folder."
        return
    fi

    # 2. Identify the associated image
    # We get the image ID directly from the container metadata
    local image_id=$(docker inspect --format='{{.Image}}' $container_id)

    # 3. Security prompt
    echo "🚨 WARNING: Cleanup for current folder"
    echo "   - Container ID: $container_id"
    echo "   - Image ID:     $image_id"
    echo ""
    read -p "Are you sure you want to delete everything? (y/N): " confirm

    # Check for 'y' or 'Y'
    if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
        echo "❌ Aborted."
        return
    fi

    # 4. Perform deletion
    echo "🗑️  Removing container..."
    docker rm -f $container_id

    echo "🗑️  Removing image..."
    # We use '|| true' so the script doesn't fail if the image is used by other containers
    docker rmi $image_id || echo "⚠️  Could not remove image (it might be in use by another project)."

    echo "✅ Clean up complete."
}

```

### Let Claude access screenshots and other data
In theory, you can paste images from the clipboard into the Claude Code shell to let it analyze them. However, that does not work reliably with devcontainers (sometimes it works, but often not).
To make screenshot sharing easier with CC, there is a readonly hosting for a sharing folder, which is setup by the devcontainer.json:
It binds your host's folder `~/.claude_transfer` to the container's `~/transfer`.

### Let Claude start and stop a test server
When Claude needs to test something "locally", it normally starts its own server - and this with a big variation of shell commands, that you 
always have to confirm. This can be very annoying, since it requires you to stare at the monitor all the time. 

To overcome this issue, there are three scripts "start-server.sh", "stop-server.sh" and "print-server-logs.sh" on which Claude has access.
I recommend you to tell your local instance to use these (and write it to the claude.md).

## Custom Agents

This template ships with 15 custom agents in `.claude/agents/` that extend Claude Code with specialized capabilities. When Claude receives a non-trivial task, it delegates to the **agents-manager**, which picks the best-fitting agent automatically.

| Agent | Purpose |
|-------|---------|
| **agents-manager** | Primary task router — delegates tasks to the right agent. Also discovers the tech stack and injects project-specific patterns into all other agents. |
| **architecture-guard** | Checks structural compliance, cross-module import violations, and package placement rules. |
| **code-reviewer** | Fast, focused code review of a diff — correctness, style, security. No builds or tests. |
| **dependency-auditor** | Audits dependencies for known CVEs, outdated versions, and license issues. |
| **devcontainer-auditor** | Audits Dockerfiles, devcontainer.json, compose files, and scripts for security and efficiency. |
| **docs-engineer** | Creates, updates, reviews, and restructures documentation (API docs, READMEs, architecture guides, CLAUDE.md). |
| **fullstack-developer** | End-to-end feature implementation spanning JPA entities, services, REST APIs, and Vaadin UI. |
| **housekeeper** | Cleans up servers, Docker containers, temp files, screenshots, and Chromium processes after development activity. |
| **migration-auditor** | Audits database migrations for destructive operations, naming issues, and backward compatibility. |
| **performance-auditor** | Static analysis for N+1 queries, memory leaks, large payloads, and inefficient rendering. |
| **qa-tester** | Comprehensive QA: code review + build + tests + responsive design checks + test gap analysis. |
| **requirements-reviewer** | Reviews feature requirements and implementation plans for completeness and feasibility before coding starts. |
| **security-reviewer** | Deep security review covering auth flows, session management, injection vectors, access control, and secrets handling. |
| **ui-designer** | Reviews UI design decisions for visual consistency, accessibility (WCAG 2.1 AA), responsive layouts, and design system adherence. |
| **ui-explorer** | Live browser-based visual testing via Playwright at mobile (375x812) and desktop (1280x800) viewports. |

> **Note:** `code-reviewer` and `qa-tester` are mutually exclusive — don't run both on the same changes. `qa-tester` includes everything `code-reviewer` does, plus builds, tests, and responsive checks.
>
> After successful runs, `qa-tester` and `ui-explorer` recommend running `housekeeper` for cleanup. The `agents-manager` enforces sequencing rules (e.g. review before implementation) and only parallelizes truly independent agents.

## Extensions
### Docker in Docker
If you need docker inside your devcontainer, e.g. for testcontainers, add this to toplevel elements in the `devcontainer.json`:

```json
  "features": {
    "ghcr.io/devcontainers/features/docker-in-docker:2": {}
  },
```

## Troubleshooting
### Outdated docker container
Sometimes it may happen, that `claude-stop` and a rebuild do not update the used docker container. For instance, when you add new dependencies or commands to the Dockerfile and nothing changes, this is an indicator for an outdated docker container. 

Check your docker container (for instance via shell using `docker container list`), if the CREATED time matches your build time. If it is older, then there might have been some hickup and you have to delete the docker container yourself (e.g. `docker container rm ID`). Then simply start it again and connect with it.

### I cannot paste images
See **Let Claude access screenshots and other data**
