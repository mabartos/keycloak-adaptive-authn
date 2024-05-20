import {useState} from "react";
import {useTranslation} from "react-i18next";
import {
    Button,
    Flex,
    FlexItem,
    Title,
    TitleSizes,
} from "@patternfly/react-core";

import type AuthenticationFlowRepresentation
    from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import {ListEmptyState} from "../components/list-empty-state/ListEmptyState";

import "./empty-execution-state.css";
import {AddSubPolicyModal, Policy} from "./components/modals/AddSubPolicyModal";
import type {
    AuthenticationProviderRepresentation
} from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import {AddStepModal} from "./components/modals/AddStepModal";

const SECTIONS = ["addExecution", "addSubFlow"] as const;
type SectionType = (typeof SECTIONS)[number] | undefined;

type EmptyAuthenticationPolicyProps = {
    policy: AuthenticationFlowRepresentation;
    onAddExecution: (type: AuthenticationProviderRepresentation) => void;
    onAddSubPolicy: (flow: Policy) => void;
    isParentPolicy: boolean;
};

export const EmptyAuthenticationPolicy = ({
                                              policy,
                                              onAddExecution,
                                              onAddSubPolicy,
                                              isParentPolicy
                                          }: EmptyAuthenticationPolicyProps) => {
    const {t} = useTranslation();
    const [show, setShow] = useState<SectionType>();

    return (
        <>
            {!isParentPolicy && show === "addExecution" && (
                <AddStepModal
                    name={policy.alias!}
                    type={"basic"}
                    onSelect={(type) => {
                        if (type) {
                            onAddExecution(type);
                        }
                        setShow(undefined);
                    }}
                />
            )}
            {show === "addSubFlow" && (
                <AddSubPolicyModal
                    name={policy.alias!}
                    onCancel={() => setShow(undefined)}
                    onConfirm={(newFlow) => {
                        onAddSubPolicy(newFlow);
                        setShow(undefined);
                    }}
                />
            )}
            <ListEmptyState
                message={t("emptyAuthenticationPolicy")}
                instructions={t("emptyAuthenticationPolicyInstructions")}
            />

            <div className="keycloak__empty-execution-state__block">
                {SECTIONS.filter(f => !isParentPolicy || f !== "addExecution").map((section) => (
                    <Flex key={section} className="keycloak__empty-execution-state__help">
                        <FlexItem flex={{default: "flex_1"}}>
                            <Title headingLevel="h2" size={TitleSizes.md}>
                                {t(`${section}Title`)}
                            </Title>
                            <p>{t(section)}</p>
                        </FlexItem>
                        <Flex alignSelf={{default: "alignSelfCenter"}}>
                            <FlexItem>
                                <Button
                                    data-testid={section}
                                    variant="tertiary"
                                    onClick={() => setShow(section)}
                                >
                                    {t(section)}
                                </Button>
                            </FlexItem>
                        </Flex>
                    </Flex>
                ))}
            </div>
        </>
    );
};
