import AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import type { AuthenticationProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import AuthenticatorConfigRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import {
    AlertVariant,
    Button,
    ButtonVariant,
    DataList,
    DragDrop,
    Droppable,
    PageSection,
    ToggleGroup,
    ToggleGroupItem,
    Toolbar,
    ToolbarContent,
    ToolbarItem,
} from "@patternfly/react-core";
import { DropdownItem } from "@patternfly/react-core/deprecated";
import { DomainIcon, TableIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { Trans, useTranslation } from "react-i18next";
import { useNavigate, useParams } from "react-router-dom";
import {useAdminClient} from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { useFetch } from "../utils/useFetch";
import useToggle from "../utils/useToggle";
import { BindFlowDialog } from "./BindFlowDialog";
import { DuplicateFlowModal } from "./DuplicateFlowModal";
import { EditFlowModal } from "./EditFlowModal";
import { FlowDiagram } from "./components/FlowDiagram";
import { FlowHeader } from "./components/FlowHeader";
import { AddStepModal } from "./components/modals/AddStepModal";
import {
    ExecutionList,
    ExpandableExecution,
    IndexChange,
    LevelChange,
} from "./execution-model";
import { toAuthentication } from "./routes/Authentication";
import {EmptyAuthenticationPolicy} from "./EmptyAuthenticationPolicy";
import {AddSubPolicyModal, Policy} from "./components/modals/AddSubPolicyModal";
import {AuthenticationPolicyParams, toAuthenticationPolicy} from "./routes/AuthenticationPolicy";
import {PolicyRow} from "./components/PolicyRow";
import {Flow} from "./components/modals/AddSubFlowModal";
import {AuthenticationPolicyHeader} from "./components/AuthenticationPolicyHeader";

export const providerConditionFilter = (
    value: AuthenticationProviderRepresentation,
) => value.displayName?.startsWith("Condition ");

type AuthenticationPolicyDetailsProps = {
    isParentPolicy?: boolean
};
export default function AuthenticationPolicyDetails({isParentPolicy = false}: AuthenticationPolicyDetailsProps) {
    const {adminClient} = useAdminClient();
    const { t } = useTranslation();
    const { realm } = useRealm();
    const { addAlert, addError } = useAlerts();
    const {id: paramId} = useParams<AuthenticationPolicyParams>();
    const navigate = useNavigate();
    const [id, setId] = useState("");
    const [key, setKey] = useState(0);
    const refresh = () => setKey(new Date().getTime());

    const [tableView, setTableView] = useState(true);
    const [policy, setPolicy] = useState<AuthenticationFlowRepresentation>();
    const [conditionList, setConditionList] = useState<ExecutionList>();
    const [liveText, setLiveText] = useState("");

    const [showAddExecutionDialog, setShowAddExecutionDialog] = useState<boolean>();
    const [showAddConditionDialog, setShowAddConditionDialog] = useState<boolean>();
    const [showAddSubFlowDialog, setShowSubFlowDialog] = useState<boolean>();
    const [selectedExecution, setSelectedExecution] =
        useState<ExpandableExecution>();
    const [open, toggleOpen, setOpen] = useToggle();
    const [edit, setEdit] = useState(false);
    const [bindFlowOpen, toggleBindFlow] = useToggle();

    useFetch(
        async () => {
            const policy = isParentPolicy ?
                await adminClient.authenticationPolicies.getParentPolicy() :
                await adminClient.authenticationPolicies.getPolicy({id: paramId!});

            if (!policy) {
                console.warn("cannot find authentication policy")
                throw new Error(t("notFound"));
            }

            setId(policy.id!);

            const executions = isParentPolicy ?
                await adminClient.authenticationPolicies.getPolicies() :
                await adminClient.authenticationManagement.getExecutions({flow: policy.alias!});

            return { policy, executions};
        },
        ({ policy, executions }) => {
            setPolicy(policy);
            setConditionList(new ExecutionList(executions));
        },
        [key],
    );

    const executeChange = async (
        ex: AuthenticationFlowRepresentation | ExpandableExecution,
        change: LevelChange | IndexChange,
    ) => {
        try {
            let id = ex.id!;
            if ("parent" in change) {
                let config: AuthenticatorConfigRepresentation = {};
                if ("authenticationConfig" in ex) {
                    config = await adminClient.authenticationManagement.getConfig({
                        id: ex.authenticationConfig as string,
                    });
                }

                try {
                    await adminClient.authenticationManagement.delExecution({ id });
                } catch {
                    // skipping already deleted execution
                }
                if ("authenticationFlow" in ex) {
                    const executionFlow = ex as ExpandableExecution;
                    const result =
                        await adminClient.authenticationManagement.addFlowToFlow({
                            flow: change.parent?.displayName! || policy?.alias!,
                            alias: executionFlow.displayName!,
                            description: executionFlow.description!,
                            provider: ex.providerId!,
                            type: "basic-flow",
                        });
                    id = result.id!;
                    ex.executionList?.forEach((e, i) =>
                        executeChange(e, {
                            parent: { ...ex, id: result.id },
                            newIndex: i,
                            oldIndex: i,
                        }),
                    );
                } else {
                    const result =
                        await adminClient.authenticationManagement.addExecutionToFlow({
                            flow: change.parent?.displayName! || policy?.alias!,
                            provider: ex.providerId!,
                        });

                    if (config.id) {
                        const newConfig = {
                            id: result.id,
                            alias: config.alias,
                            config: config.config,
                        };
                        await adminClient.authenticationManagement.createConfig(newConfig);
                    }

                    id = result.id!;
                }
            }
            const times = change.newIndex - change.oldIndex;
            for (let index = 0; index < Math.abs(times); index++) {
                if (times > 0) {
                    await adminClient.authenticationManagement.lowerPriorityExecution({
                        id,
                    });
                } else {
                    await adminClient.authenticationManagement.raisePriorityExecution({
                        id,
                    });
                }
            }
            refresh();
            addAlert(t("updateFlowSuccess"), AlertVariant.success);
        } catch (error: any) {
            addError("updateFlowError", error);
        }
    };

    const update = async (execution: ExpandableExecution) => {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const { executionList, isCollapsed, ...ex } = execution;
        try {
            await adminClient.authenticationManagement.updateExecution(
                {flow: policy?.alias!},
                ex,
            );
            refresh();
            addAlert(t("updateFlowSuccess"), AlertVariant.success);
        } catch (error: any) {
            addError("updateFlowError", error);
        }
    };

    const addExecution = async (
        name: string,
        type: AuthenticationProviderRepresentation,
    ) => {
        try {
            await adminClient.authenticationManagement.addExecutionToFlow({
                flow: name,
                provider: type.id!
            });
            refresh();
            addAlert(t("updateFlowSuccess"), AlertVariant.success);
        } catch (error) {
            addError("updateFlowError", error);
        }
    };

    const addPolicy = async (
        flow: string,
        {name, description = "", providerId = "basic-flow"}: Policy,
    ) => {
        try {
            await adminClient.authenticationPolicies.createPolicy({
                alias: name,
                description,
                providerId
            });
            refresh();
            addAlert(t("updateFlowSuccess"), AlertVariant.success);
        } catch (error) {
            addError("updateFlowError", error);
        }
    };

    const addFlow = async (
        flow: string,
        {name, description = "", providerId = "basic-flow"}: Policy,
    ) => {
        try {
            await adminClient.authenticationManagement.addFlowToFlow({
                flow,
                alias: name,
                description,
                provider: providerId,
                type: "basic-flow",
            });
            refresh();
            addAlert(t("updateFlowSuccess"), AlertVariant.success);
        } catch (error) {
            addError("updateFlowError", error);
        }
    };

    const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
        titleKey: "deleteConfirmExecution",
        children: (
            <Trans i18nKey="deleteConfirmExecutionMessage">
                {" "}
                <strong>{{ name: selectedExecution?.displayName }}</strong>.
            </Trans>
        ),
        continueButtonLabel: "delete",
        continueButtonVariant: ButtonVariant.danger,
        onConfirm: async () => {
            try {
                await adminClient.authenticationManagement.delExecution({
                    id: selectedExecution?.id!,
                });
                addAlert(t("deleteExecutionSuccess"), AlertVariant.success);
                refresh();
            } catch (error) {
                addError("deleteExecutionError", error);
            }
        },
    });

    const [toggleDeleteFlow, DeleteFlowConfirm] = useConfirmDialog({
        titleKey: "deleteConfirmFlow",
        children: (
            <Trans i18nKey="deleteConfirmFlowMessage">
                {" "}
                <strong>{{flow: policy?.alias || ""}}</strong>.
            </Trans>
        ),
        continueButtonLabel: "delete",
        continueButtonVariant: ButtonVariant.danger,
        onConfirm: async () => {
            try {
                await adminClient.authenticationPolicies.deletePolicy({
                    flowId: policy!.id!,
                });
                navigate(toAuthentication({ realm }));
                addAlert(t("deleteFlowSuccess"), AlertVariant.success);
            } catch (error) {
                addError("deleteFlowError", error);
            }
        },
    });

    const hasExecutions = conditionList?.expandableList.length !== 0;

    const dropdownItems = [
        <DropdownItem key="duplicate" onClick={() => setOpen(true)}>
            {t("duplicate")}
        </DropdownItem>,

        <DropdownItem
            data-testid="edit-flow"
            key="edit"
            onClick={() => setEdit(true)}
        >
            {t("editInfo")}
        </DropdownItem>,
        <DropdownItem
            data-testid="delete-flow"
            key="delete"
            onClick={() => toggleDeleteFlow()}
        >
            {t("delete")}
        </DropdownItem>,
    ];

    return (
        <>
            {bindFlowOpen && (
                <BindFlowDialog
                    flowAlias={policy?.alias!}
                    onClose={() => {
                        toggleBindFlow();
                        navigate(
                            toAuthenticationPolicy({
                                realm,
                                id: id!
                            }),
                        );
                    }}
                />
            )}
            {open && (
                <DuplicateFlowModal
                    name={policy?.alias!}
                    description={policy?.description!}
                    toggleDialog={toggleOpen}
                    onComplete={() => {
                        refresh();
                        setOpen(false);
                    }}
                />
            )}
            {edit && (
                <EditFlowModal
                    flow={policy!}
                    toggleDialog={() => {
                        setEdit(!edit);
                        refresh();
                    }}
                />
            )}
            <DeleteFlowConfirm />

            {!isParentPolicy && (
                <>
                    <ViewHeader
                        titleKey={policy?.alias || ""}
                        dropdownItems={dropdownItems}
                    />
                </>
            )}

            <PageSection variant="light">
                {conditionList && hasExecutions && (
                    <>
                        <Toolbar id="toolbar">
                            <ToolbarContent>
                                <ToolbarItem>
                                    <ToggleGroup>
                                        <ToggleGroupItem
                                            icon={<TableIcon />}
                                            aria-label={t("tableView")}
                                            buttonId="tableView"
                                            isSelected={tableView}
                                            onChange={() => setTableView(true)}
                                        />
                                        {!isParentPolicy && (
                                            <ToggleGroupItem
                                                icon={<DomainIcon/>}
                                                aria-label={t("diagramView")}
                                                buttonId="diagramView"
                                                isSelected={!tableView}
                                                onChange={() => setTableView(false)}
                                            />
                                        )}
                                    </ToggleGroup>
                                </ToolbarItem>
                                {!isParentPolicy && (
                                    <>
                                        <ToolbarItem>
                                            <Button
                                                data-testid="addStep"
                                                variant="secondary"
                                                onClick={() => setShowAddExecutionDialog(true)}
                                            >
                                                {t("addStep")}
                                            </Button>
                                        </ToolbarItem>
                                        <ToolbarItem>
                                            <Button
                                                data-testid="addCondition"
                                                variant="secondary"
                                                onClick={() => setShowAddConditionDialog(true)}
                                            >
                                                {t("addCondition")}
                                            </Button>
                                        </ToolbarItem>
                                    </>
                                )}
                                <ToolbarItem>
                                    <Button
                                        data-testid="addPolicy"
                                        variant="secondary"
                                        onClick={() => setShowSubFlowDialog(true)}
                                    >
                                        {t("addPolicy")}
                                    </Button>
                                </ToolbarItem>
                            </ToolbarContent>
                        </Toolbar>
                        <DeleteConfirm />
                        {tableView && (
                            <DragDrop
                                onDrag={({ index }) => {
                                    const item = conditionList.findExecution(index)!;
                                    setLiveText(t("onDragStart", { item: item.displayName }));
                                    if (!item.isCollapsed) {
                                        item.isCollapsed = true;
                                        setConditionList(conditionList.clone());
                                    }
                                    return true;
                                }}
                                onDragMove={({ index }) => {
                                    const dragged = conditionList.findExecution(index);
                                    setLiveText(t("onDragMove", { item: dragged?.displayName }));
                                }}
                                onDrop={(source, dest) => {
                                    if (dest) {
                                        const dragged = conditionList.findExecution(source.index)!;
                                        const order = conditionList.order().map((ex) => ex.id!);
                                        setLiveText(
                                            t("onDragFinish", { list: dragged.displayName }),
                                        );

                                        const [removed] = order.splice(source.index, 1);
                                        order.splice(dest.index, 0, removed);
                                        const change = conditionList.getChange(dragged, order);
                                        executeChange(dragged, change);
                                        return true;
                                    } else {
                                        setLiveText(t("onDragCancel"));
                                        return false;
                                    }
                                }}
                            >
                                <Droppable hasNoWrapper>
                                    <DataList aria-label={t("flows")}>
                                        <AuthenticationPolicyHeader isParentPolicy={isParentPolicy}/>
                                        <>
                                            {conditionList.expandableList.map((execution) => (
                                                <PolicyRow
                                                    builtIn={false}
                                                    isParentPolicy={isParentPolicy}
                                                    key={execution.id}
                                                    execution={execution}
                                                    onRowClick={(execution) => {
                                                        execution.isCollapsed = !execution.isCollapsed;
                                                        setConditionList(conditionList.clone());
                                                    }}
                                                    onRowChange={update}
                                                    onAddExecution={(execution, type) =>
                                                        addExecution(execution.displayName!, type)
                                                    }
                                                    onDelete={(execution) => {
                                                        setSelectedExecution(execution);
                                                        toggleDeleteDialog();
                                                    }}
                                                />
                                            ))}
                                        </>
                                    </DataList>
                                </Droppable>
                            </DragDrop>
                        )}
                        {policy && (
                            <>
                                {!isParentPolicy && showAddExecutionDialog && (
                                    <AddStepModal
                                        name={policy.alias!}
                                        type={"basic"}
                                        onSelect={(type) => {
                                            if (type) {
                                                addExecution(policy.alias!, type);
                                            }
                                            setShowAddExecutionDialog(false);
                                        }}
                                    />
                                )}
                                {!isParentPolicy && showAddConditionDialog && (
                                    <AddStepModal
                                        name={policy.alias!}
                                        type={"condition"}
                                        onSelect={(type) => {
                                            if (type) {
                                                addExecution(policy.alias!, type);
                                            }
                                            setShowAddConditionDialog(false);
                                        }}
                                    />
                                )}
                                {showAddSubFlowDialog && (
                                    <AddSubPolicyModal
                                        name={policy.alias!}
                                        onCancel={() => setShowSubFlowDialog(false)}
                                        onConfirm={(newFlow) => {
                                            isParentPolicy ? addPolicy(policy.alias!, newFlow) : addFlow(policy.alias!, newFlow);
                                            setShowSubFlowDialog(false);
                                        }}
                                    />
                                )}
                            </>
                        )}
                        <div className="pf-v5-screen-reader" aria-live="assertive">
                            {liveText}
                        </div>
                    </>
                )}
                {!tableView && conditionList?.expandableList && (
                    <FlowDiagram executionList={conditionList}/>
                )}
                {!conditionList?.expandableList ||
                    (policy && !hasExecutions && (
                        <EmptyAuthenticationPolicy
                            isParentPolicy={isParentPolicy}
                            policy={policy}
                            onAddExecution={(type) => addExecution(policy.alias!, type)}
                            onAddCondition={(type) => addExecution(policy.alias!, type)}
                            onAddSubPolicy={(newFlow) => isParentPolicy ? addPolicy(policy.alias!, newFlow) : addFlow(policy.alias!, newFlow)}
                        />
                    ))}
            </PageSection>
        </>
    );
}
